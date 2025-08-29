import {useNavigate} from 'react-router-dom';

export default function NotFound() {
  const navigate = useNavigate();
  const goMain = () => navigate("/", { replace: true });

  return (
    <section className="flex items-center justify-center min-h-screen bg-background-light dark:bg-background-dark transition-colors duration-300 px-4">
      <div className="text-center max-w-md">
        <h1 className="text-6xl font-bold text-accent-light dark:text-accent-dark mb-4">404</h1>
        <p className="text-lg text-text-light dark:text-text-dark mb-6">
          페이지를 찾을 수 없습니다.
        </p>
        <button
          onClick={goMain}
          className="px-6 py-2 bg-primary-light dark:bg-primary-dark text-background-light dark:text-background-dark rounded-md hover:bg-primary-dark dark:hover:bg-primary-light transition-colors duration-300">
          메인으로 돌아가기
        </button>
      </div>
    </section>
  );
}