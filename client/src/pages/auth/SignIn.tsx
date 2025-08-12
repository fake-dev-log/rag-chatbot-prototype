import { useForm } from 'react-hook-form';
import { useLogin } from '@apis/hooks/auth.ts';
import type { LoginRequest } from '@apis/types/auth.ts';
import Logo from "@components/Logo.tsx";
import {APP_NAME} from "@constants";

export default function SignIn() {
  const { register, handleSubmit } = useForm<LoginRequest>();
  const login = useLogin();
  const { isPending } = login;

  const onSubmit = (data: LoginRequest) => {
    login.mutate(data);
  };

  return (
    <div className="flex flex-col h-screen w-full bg-background-light dark:bg-background-dark items-center justify-center p-10 transition-colors duration-300">
      <div className="w-full max-w-md">
        <div className="flex flex-col p-8 items-center">
          <Logo size={64} />
          <h1 className="text-5xl font-bold mt-4 text-gray-800 dark:text-gray-100">{APP_NAME}</h1>
        </div>
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-surface-light dark:bg-surface-dark rounded-lg shadow-md p-8 space-y-8 transition-colors duration-300"
        >
          <input
            type="email"
            placeholder="이메일"
            {...register('email', {
              required: true,
              pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
            })}
            autoFocus
            className="
              w-full px-4 py-4 rounded-lg border
              bg-surface-light text-text-light border-secondary-light
              focus:outline-none focus:ring-2 focus:ring-primary-light dark:focus:ring-primary-dark
              dark:bg-surface-dark dark:text-text-dark dark:border-secondary-dark
              disabled:opacity-50 transition-colors duration-300
            "
          />

          <input
            type="password"
            placeholder="비밀번호"
            {...register('password', { required: true })}
            className="
              w-full px-4 py-4 rounded-lg border
              bg-surface-light text-text-light border-secondary-light
              focus:outline-none focus:ring-2 focus:ring-primary-light dark:focus:ring-primary-dark
              dark:bg-surface-dark dark:text-text-dark dark:border-secondary-dark
              disabled:opacity-50 transition-colors duration-300
            "
          />

          <button
            type="submit"
            disabled={isPending}
            className="
              w-full py-3 rounded-lg text-surface-light
              bg-primary-light hover:bg-primary-dark
              dark:bg-primary-dark dark:hover:bg-primary-light
              disabled:opacity-50 transition-colors duration-300
            "
          >
            {isPending ? '로그인 중…' : '로그인'}
          </button>
        </form>
      </div>
    </div>
  );
}
