import { HttpInterceptorFn } from '@angular/common/http';

const XSRF_COOKIE_NAME = 'XSRF-TOKEN';
const XSRF_HEADER_NAME = 'X-XSRF-TOKEN';
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`));
  return match ? decodeURIComponent(match[1]) : null;
}

// A sessão do backend é baseada em cookie (login via Google OAuth2), não em token —
// toda chamada precisa de withCredentials pro cookie de sessão ir junto em outra origem.
//
// O X-XSRF-TOKEN NÃO é anexado automaticamente pelo interceptor nativo do Angular
// aqui: o código-fonte dele (xsrfInterceptorFn, @angular/common/http) só age quando
// `new URL(req.url).origin === location.origin` — como o backend fica numa origem
// diferente (porta 8080 vs 4200), essa checagem falha pra toda chamada da API e o
// interceptor nativo vira um no-op silencioso (sem erro nenhum, só não manda o
// header). Por isso replicamos a lógica aqui manualmente, sem a checagem de origem
// (sabemos que XSRF_COOKIE_NAME sempre vem do nosso próprio backend).
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  let request = req.clone({ withCredentials: true });

  if (MUTATING_METHODS.has(request.method) && !request.headers.has(XSRF_HEADER_NAME)) {
    const token = readCookie(XSRF_COOKIE_NAME);
    if (token) {
      request = request.clone({ headers: request.headers.set(XSRF_HEADER_NAME, token) });
    }
  }

  return next(request);
};
