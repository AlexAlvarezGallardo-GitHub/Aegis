import { Pipe, PipeTransform } from '@angular/core';

const CURRENCY_SYMBOLS: Record<string, string> = {
  EUR: '€',
  USD: '$',
  GBP: '£',
};

@Pipe({ name: 'aegisCurrency', standalone: true })
export class AegisCurrencyPipe implements PipeTransform {
  transform(value: number | null | undefined, currency = 'USD'): string {
    const amount = value ?? 0;
    const prefix = amount < 0 ? '-' : '';
    const abs = Math.abs(amount);
    const symbol = CURRENCY_SYMBOLS[currency] ?? (currency ? `${currency} ` : '$');
    return (
      prefix +
      symbol +
      abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    );
  }
}

export function formatMoney(value: number | null | undefined, currency = 'USD'): string {
  return new AegisCurrencyPipe().transform(value, currency);
}
