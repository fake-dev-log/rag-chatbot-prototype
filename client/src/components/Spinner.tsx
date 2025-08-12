export function Spinner({ size = 5 }: {size?: number}) {
  return (
    <div
      className={
        `inline-block w-${size.toString()} h-${size.toString()} rounded-full border-${Math.ceil(size/2).toString()} border-transparent
         border-r-primary-light dark:border-r-primary-dark
         animate-spin transition-colors duration-300`
      }
    />
  );
}
