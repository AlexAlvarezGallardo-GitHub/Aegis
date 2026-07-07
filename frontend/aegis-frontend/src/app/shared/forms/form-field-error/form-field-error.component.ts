import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-aegis-form-field-error',
  standalone: true,
  imports: [CommonModule, MatFormFieldModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (shouldShow()) {
      <mat-error>{{ errorMessage() }}</mat-error>
    }
  `,
})
export class FormFieldErrorComponent {
  readonly form = input.required<FormGroup>();
  readonly controlName = input.required<string>();
  readonly labels = input<Record<string, string>>({});

  readonly shouldShow = computed<boolean>(() => {
    const form = this.form();
    const name = this.controlName();
    const control = form.get(name);
    return !!control && control.invalid && control.touched;
  });

  readonly errorMessage = computed<string>(() => {
    const form = this.form();
    const name = this.controlName();
    const control = form.get(name);
    const labels = this.labels();

    if (!control || !control.errors) return '';

    const fieldLabel = labels[name] || name;

    if (control.errors['required']) {
      return `${fieldLabel} is required`;
    }
    if (control.errors['email']) {
      return 'Please enter a valid email address';
    }
    if (control.errors['minlength']) {
      return `Minimum ${control.errors['minlength'].requiredLength} characters required`;
    }
    if (control.errors['maxlength']) {
      return `Maximum ${control.errors['maxlength'].requiredLength} characters allowed`;
    }
    if (control.errors['min']) {
      return `Minimum value is ${control.errors['min'].min}`;
    }
    if (control.errors['max']) {
      return `Maximum value is ${control.errors['max'].max}`;
    }
    if (control.errors['pattern']) {
      return `Invalid ${fieldLabel} format`;
    }

    return '';
  });
}
