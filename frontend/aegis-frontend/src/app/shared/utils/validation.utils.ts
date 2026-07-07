import { AbstractControl, ValidationErrors, ValidatorFn, FormGroup } from '@angular/forms';

export function emailValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return emailRegex.test(control.value) ? null : { email: true };
  };
}

export function passwordStrengthValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const value = control.value as string;
    const errors: ValidationErrors = {};
    if (value.length < 8) errors['minlength'] = { requiredLength: 8, actualLength: value.length };
    if (value.length > 128) errors['maxlength'] = { requiredLength: 128, actualLength: value.length };
    return Object.keys(errors).length > 0 ? errors : null;
  };
}

export function currencyCodeValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const currencyRegex = /^[A-Z]{3}$/;
    return currencyRegex.test(control.value) ? null : { pattern: true };
  };
}

export function getErrorMessage(
  control: AbstractControl | null,
  labels: Record<string, string> = {}
): string {
  if (!control || !control.errors) return '';

  const name = ((control as unknown) as Record<string, unknown>)['name'] as string || '';
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
}

export function markFormGroupTouched(form: AbstractControl): void {
  form.markAllAsTouched();
  if (form instanceof FormGroup) {
    Object.values(form.controls).forEach((control) => {
      if (control instanceof FormGroup) {
        markFormGroupTouched(control);
      } else {
        control.markAsTouched();
      }
    });
  }
}
