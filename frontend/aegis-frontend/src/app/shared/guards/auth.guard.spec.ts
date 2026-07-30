import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../../features/auth/auth.service';
import * as environment from '../../../environments/environment';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const mockRoute = {} as ActivatedRouteSnapshot;

  function mockState(url: string): RouterStateSnapshot {
    return { url } as RouterStateSnapshot;
  }

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated', 'checkSession']);
    const routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    guard = TestBed.inject(AuthGuard);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    (environment.environment as { enableMockLogin: boolean }).enableMockLogin = false;
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow activation when authenticated', (done) => {
    authService.isAuthenticated.and.returnValue(true);

    guard.canActivate(mockRoute, mockState('/wallets')).subscribe((result) => {
      expect(result).toBe(true);
      expect(router.createUrlTree).not.toHaveBeenCalled();
      done();
    });
  });

  it('should allow activation when session check succeeds', (done) => {
    authService.isAuthenticated.and.returnValue(false);
    authService.checkSession.and.returnValue(of(true));

    guard.canActivate(mockRoute, mockState('/wallets')).subscribe((result) => {
      expect(result).toBe(true);
      expect(authService.checkSession).toHaveBeenCalled();
      done();
    });
  });

  it('should redirect to login with return URL when not authenticated', (done) => {
    authService.isAuthenticated.and.returnValue(false);
    authService.checkSession.and.returnValue(of(false));
    router.createUrlTree.and.returnValue({} as UrlTree);

    guard.canActivate(mockRoute, mockState('/wallets')).subscribe((result) => {
      expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/wallets' },
      });
      expect(result).toEqual({} as UrlTree);
      done();
    });
  });

  it('should redirect to login with nested return URL', (done) => {
    authService.isAuthenticated.and.returnValue(false);
    authService.checkSession.and.returnValue(of(false));
    router.createUrlTree.and.returnValue({} as UrlTree);

    guard.canActivate(mockRoute, mockState('/settings/profile')).subscribe(() => {
      expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/settings/profile' },
      });
      done();
    });
  });

  it('should not call checkSession when already authenticated', (done) => {
    authService.isAuthenticated.and.returnValue(true);

    guard.canActivate(mockRoute, mockState('/wallets')).subscribe(() => {
      expect(authService.checkSession).not.toHaveBeenCalled();
      done();
    });
  });
});
