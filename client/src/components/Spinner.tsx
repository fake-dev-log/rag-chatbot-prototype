export function Spinner() {
  return (
    <div
      className={
        `inline-block w-5 h-5 rounded-full border-3 border-transparent
         border-r-primary-light dark:border-r-primary-dark
         animate-spin transition-colors duration-300`
      }
    />
  );
}
