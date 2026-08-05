import { Injectable, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, Subject, merge, of } from 'rxjs';
import { catchError, map, shareReplay, switchMap, startWith } from 'rxjs/operators';
import { DashboardData, TimeRange } from '../../shared/models/dashboard.model';

export interface DashboardState {
  data: DashboardData | null;
  loading: boolean;
  error: string | null;
  timeRange: TimeRange;
}

interface DashboardResult {
  data: DashboardData | null;
  error: string | null;
}

const REFRESH_INTERVAL_MS = 30_000;
const ERROR_MESSAGE = 'Metrics temporarily unavailable';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  private readonly timeRange = signal<TimeRange>('30d');
  private readonly manualRefresh = new Subject<void>();
  private readonly polling$: Observable<DashboardResult>;

  readonly state = signal<DashboardState>({
    data: null,
    loading: true,
    error: null,
    timeRange: '30d',
  });

  constructor() {
    this.polling$ = merge(
      toObservable(this.timeRange).pipe(takeUntilDestroyed(this.destroyRef)),
      this.manualRefresh.pipe(takeUntilDestroyed(this.destroyRef)),
      interval(REFRESH_INTERVAL_MS).pipe(takeUntilDestroyed(this.destroyRef)),
    ).pipe(
      startWith(void 0),
      switchMap(() => this.fetchDashboardData(this.timeRange())),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    this.polling$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      this.state.set({
        data: result.data,
        loading: false,
        error: result.error,
        timeRange: this.timeRange(),
      });
    });
  }

  setTimeRange(range: TimeRange): void {
    this.timeRange.set(range);
    this.state.update((s) => ({ ...s, loading: true }));
  }

  refresh(): void {
    this.manualRefresh.next();
  }

  private fetchDashboardData(range: TimeRange): Observable<DashboardResult> {
    return this.http.get<DashboardData>('/api/bff/dashboard', {
      params: { range },
    }).pipe(
      map((data) => ({
        data: { ...data, lastUpdated: new Date().toISOString() },
        error: null,
      })),
      catchError(() => of({ data: null, error: ERROR_MESSAGE })),
    );
  }
}
