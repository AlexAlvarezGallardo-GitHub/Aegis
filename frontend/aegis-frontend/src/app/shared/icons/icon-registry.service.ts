import { Injectable, inject } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

@Injectable({ providedIn: 'root' })
export class IconRegistryService {
  private registry = inject(MatIconRegistry);
  private sanitizer = inject(DomSanitizer);

  private customIcons: Record<string, string> = {
    'aegis-shield': 'shield.svg',
    'aegis-wallet': 'wallet.svg',
    'aegis-payment': 'payment.svg',
    'aegis-fraud-alert': 'fraud-alert.svg',
    'aegis-transaction': 'transaction.svg',
    'aegis-empty-data': 'empty-data.svg',
    'aegis-error-state': 'error-state.svg',
    'aegis-success-state': 'success-state.svg',
    'aegis-maintenance': 'maintenance.svg',
  };

  register(): void {
    Object.entries(this.customIcons).forEach(([name, file]) => {
      this.registry.addSvgIcon(
        name,
        this.sanitizer.bypassSecurityTrustResourceUrl(`/assets/icons/${file}`)
      );
    });
  }
}
