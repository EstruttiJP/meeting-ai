import { HttpInterceptorFn } from '@angular/common/http';

// A sessão do backend é baseada em cookie (login via Google OAuth2), não em token —
// toda chamada precisa mandar credenciais para o cookie de sessão (e o XSRF-TOKEN,
// lido automaticamente pelo Angular) chegarem no backend em outra origem.
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
