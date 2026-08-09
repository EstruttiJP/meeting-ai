import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MeetingStatusBadge } from './meeting-status-badge';

describe('MeetingStatusBadge', () => {
  let fixture: ComponentFixture<MeetingStatusBadge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MeetingStatusBadge],
    }).compileComponents();

    fixture = TestBed.createComponent(MeetingStatusBadge);
  });

  it('renders a Portuguese label for each meeting status', () => {
    fixture.componentRef.setInput('status', 'READY');
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Pronta para revisão');
  });

  it('renders the failed status with a danger label', () => {
    fixture.componentRef.setInput('status', 'FAILED');
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Falhou');
  });
});
