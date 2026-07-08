import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ConfirmationDialogComponent, ConfirmationData } from './confirmation-dialog.component';

describe('ConfirmationDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmationDialogComponent>;
  let component: ConfirmationDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<DialogRef<boolean>>;

  const defaultData: ConfirmationData = {
    title: 'Confirm Action',
    message: 'Are you sure?',
    confirmText: 'Yes',
    cancelText: 'No',
    destructive: false,
  };

  function createComponent(data: ConfirmationData = defaultData): void {
    dialogRefSpy = jasmine.createSpyObj<DialogRef<boolean>>('DialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [ConfirmationDialogComponent],
      providers: [
        { provide: DIALOG_DATA, useValue: data },
        { provide: DialogRef, useValue: dialogRefSpy },
      ],
    });
    fixture = TestBed.createComponent(ConfirmationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('should create', () => {
    createComponent();
    expect(component).toBeTruthy();
  });

  it('should display the title', () => {
    createComponent();
    const titleEl = fixture.nativeElement.querySelector('#dialog-title');
    expect(titleEl.textContent).toContain('Confirm Action');
  });

  it('should display the message', () => {
    createComponent();
    const msgEl = fixture.nativeElement.querySelector('#dialog-message');
    expect(msgEl.textContent).toContain('Are you sure?');
  });

  it('should display confirm and cancel buttons with correct labels', () => {
    createComponent();
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const buttonTexts = Array.from(buttons).map((b) => (b as HTMLElement).textContent?.trim());
    expect(buttonTexts).toContain('Yes');
    expect(buttonTexts).toContain('No');
  });

  it('should close with true on confirm', () => {
    createComponent();
    component.confirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });

  it('should close with false on cancel', () => {
    createComponent();
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(false);
  });

  it('should show warning icon when destructive is true', () => {
    createComponent({ ...defaultData, destructive: true });
    const icon = fixture.nativeElement.querySelector('.dialog-warning-icon');
    expect(icon).toBeTruthy();
  });

  it('should NOT show warning icon when destructive is false', () => {
    createComponent({ ...defaultData, destructive: false });
    const icon = fixture.nativeElement.querySelector('.dialog-warning-icon');
    expect(icon).toBeFalsy();
  });

  it('should have destructive class on confirm button when destructive is true', () => {
    createComponent({ ...defaultData, destructive: true });
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const confirmBtn = Array.from(buttons).find((b) => (b as HTMLElement).textContent?.trim() === 'Yes') as HTMLElement | undefined;
    expect(confirmBtn?.classList.contains('destructive')).toBeTrue();
  });

  it('should have role alertdialog', () => {
    createComponent();
    const panel = fixture.nativeElement.querySelector('.dialog-panel');
    expect(panel.getAttribute('role')).toBe('alertdialog');
  });

  it('should have aria-labelledby pointing to title', () => {
    createComponent();
    const panel = fixture.nativeElement.querySelector('.dialog-panel');
    expect(panel.getAttribute('aria-labelledby')).toBe('dialog-title');
  });

  it('should have aria-describedby pointing to message', () => {
    createComponent();
    const panel = fixture.nativeElement.querySelector('.dialog-panel');
    expect(panel.getAttribute('aria-describedby')).toBe('dialog-message');
  });

  it('should close with false on backdrop click', () => {
    createComponent();
    const overlay = fixture.nativeElement.querySelector('.dialog-overlay');
    overlay.click();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(false);
  });

  it('should NOT close on panel click (stopPropagation)', () => {
    createComponent();
    const panel = fixture.nativeElement.querySelector('.dialog-panel');
    panel.click();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
  });
});
