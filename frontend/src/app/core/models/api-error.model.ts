// Formato RFC 7807 (ProblemDetail) devolvido pelo ApiExceptionHandler do backend.
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: string[];
}

// Forma normalizada que os componentes de tela consomem — sempre uma mensagem
// pronta para exibir ao usuário, já com o "detail"/"errors" do backend resolvidos.
export interface AppError {
  status: number;
  message: string;
}
