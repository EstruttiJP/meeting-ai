import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  it('stays on the login screen when there is no active session', () => {
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/users/me').flush('unauthorized', {
      status: 401,
      statusText: 'Unauthorized',
    });

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('redirects to the dashboard when a session already exists', () => {
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();

    httpMock.expectOne('http://localhost:8080/api/users/me').flush({
      id: 'u1',
      email: 'dev@example.com',
      name: 'Dev',
      pictureUrl: null,
      createdAt: new Date().toISOString(),
    });

    expect(navigateSpy).toHaveBeenCalledWith('/dashboard');
  });
});
