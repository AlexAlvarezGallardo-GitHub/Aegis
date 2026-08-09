import { ApplicationConfig, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { httpAuthInterceptor, httpErrorInterceptor, httpTimeoutInterceptor } from './shared/interceptors';
import { IconRegistryService } from './shared/icons/icon-registry.service';

function initializeIcons(registry: IconRegistryService): () => void {
  return () => registry.register();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([httpTimeoutInterceptor, httpAuthInterceptor, httpErrorInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeIcons,
      deps: [IconRegistryService],
      multi: true,
    },
  ],
};
