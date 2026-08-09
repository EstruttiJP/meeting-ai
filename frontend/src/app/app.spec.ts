import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
    httpMock.expectOne('http://localhost:8080/api/health').flush({ status: 'UP', service: 'meeting-ai-backend', timestamp: '' });
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    httpMock.expectOne('http://localhost:8080/api/health').flush({ status: 'UP', service: 'meeting-ai-backend', timestamp: '' });
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Meeting AI');
  });
});
