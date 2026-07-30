import { Injectable, inject } from '@angular/core';
import {
  CanActivate,
  Router,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../../features/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  private authService = inject(AuthService);
  private router = inject(Router);

  canActivate(
    _route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): Observable<boolean | UrlTree> {
    if (this.authService.isAuthenticated()) {
      return of(true);
    }

    return this.authService.checkSession().pipe(
      map((authenticated) => {
        if (authenticated) {
          return true;
        }
        return this.router.createUrlTree(['/login'], {
          queryParams: { returnUrl: state.url },
        });
      }),
    );
  }
}
