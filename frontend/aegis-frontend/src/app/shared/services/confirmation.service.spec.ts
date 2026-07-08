import { TestBed } from '@angular/core/testing';
import { Dialog, DialogRef } from '@angular/cdk/dialog';
import { of } from 'rxjs';
import { ConfirmationService } from './confirmation.service';
import { ConfirmationDialogComponent } from '../components/confirmation-dialog/confirmation-dialog.component';

describe('ConfirmationService', () => {
  let service: ConfirmationService;
  let dialogSpy: jasmine.SpyObj<Dialog>;

  function mockDialogRef(closedValue: boolean): DialogRef<unknown, unknown> {
    return { closed: of(closedValue) } as unknown as DialogRef<unknown, unknown>;
  }

  beforeEach(() => {
    dialogSpy = jasmine.createSpyObj<Dialog>('Dialog', ['open']);
    TestBed.configureTestingModule({
      providers: [
        ConfirmationService,
        { provide: Dialog, useValue: dialogSpy },
      ],
    });
    service = TestBed.inject(ConfirmationService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should open confirmation dialog with provided options', () => {
    dialogSpy.open.and.returnValue(mockDialogRef(true));

    service.confirm({
      title: 'Delete',
      message: 'Delete this item?',
      confirmText: 'Delete',
      cancelText: 'Keep',
      destructive: true,
    });

    expect(dialogSpy.open).toHaveBeenCalledWith(
      ConfirmationDialogComponent,
      jasmine.objectContaining({
        data: {
          title: 'Delete',
          message: 'Delete this item?',
          confirmText: 'Delete',
          cancelText: 'Keep',
          destructive: true,
        },
        disableClose: false,
        hasBackdrop: true,
      }),
    );
  });

  it('should use default values when options are not provided', () => {
    dialogSpy.open.and.returnValue(mockDialogRef(true));

    service.confirm({
      title: 'Confirm',
      message: 'Are you sure?',
    });

    expect(dialogSpy.open).toHaveBeenCalledWith(
      ConfirmationDialogComponent,
      jasmine.objectContaining({
        data: {
          title: 'Confirm',
          message: 'Are you sure?',
          confirmText: 'Confirm',
          cancelText: 'Cancel',
          destructive: false,
        },
      }),
    );
  });

  it('should return the dialog closed observable', (done) => {
    dialogSpy.open.and.returnValue(mockDialogRef(true));

    service.confirm({
      title: 'Test',
      message: 'Test?',
    }).subscribe((result) => {
      expect(result).toBeTrue();
      done();
    });
  });

  it('should pass disableClose option to dialog config', () => {
    dialogSpy.open.and.returnValue(mockDialogRef(false));

    service.confirm({
      title: 'Test',
      message: 'Test?',
      disableClose: true,
    });

    expect(dialogSpy.open).toHaveBeenCalledWith(
      jasmine.any(Function),
      jasmine.objectContaining({ disableClose: true }),
    );
  });
});
