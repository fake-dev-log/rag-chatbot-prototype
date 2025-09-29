import { type FormEvent } from 'react';
import { ChevronDownIcon, Send } from 'lucide-react';
import { Spinner } from '@components/Spinner';

interface ChatInputProps {
  query: string;
  setQuery: (q: string) => void;
  category: string;
  setCategory: (c: string) => void;
  categories: string[];
  isStreaming: boolean; // Changed from isPending
  onSubmit: (e: FormEvent<HTMLFormElement>) => void;
}

export default function ChatInput(
  { query, setQuery, category, setCategory, categories, isStreaming, onSubmit }: ChatInputProps
) {
  return (
    <div className="w-full max-w-3xl mx-auto">
      {/* Unified container with border and focus styles */}
      <div className="relative rounded-lg border border-secondary-light dark:border-secondary-dark bg-surface-light dark:bg-surface-dark focus-within:ring-2 focus-within:ring-primary-light dark:focus-within:ring-primary-dark transition-all duration-300">
        
        {/* Text input form */}
        <form onSubmit={onSubmit}>
          <div className="flex">
            <textarea
              className="w-full p-3 pr-16 bg-transparent text-text-light dark:text-text-dark focus:outline-none resize-none placeholder:text-gray-500 dark:placeholder:text-gray-400 placeholder:text-base"
              placeholder="What are you curious about?"
              value={query}
              onChange={e => setQuery(e.target.value)}
              disabled={isStreaming} // Use isStreaming
              autoFocus
              rows={1}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  onSubmit(e as any);
                }
              }}
            />
            <button
              type="submit"
              disabled={isStreaming || !query.trim()} // Use isStreaming
              className="py-2 px-4 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 disabled:opacity-50 disabled:text-gray-400 transition-colors duration-300"
            >
              {isStreaming ? <Spinner /> : <Send className='stroke-primary-light dark:stroke-primary-dark' size={20} />}
            </button>
          </div>
        </form>

        {/* Separator */}
        <hr className="border-gray-200 dark:border-gray-700 mx-3"/>

        {/* Toolbox Area */}
        <div className="mt-2 mb-2 ml-3 flex items-center space-x-2">
          <div className="relative inline-block">
            <select
              value={category}
              onChange={e => setCategory(e.target.value)}
              disabled={isStreaming} // Use isStreaming
              className="appearance-none cursor-pointer flex items-center space-x-2 pl-3 pr-8 py-1.5 border rounded-full bg-gray-100 dark:bg-gray-700 border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <option value="">All Documents</option>
              {categories.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
            <ChevronDownIcon className="w-4 h-4 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-gray-500" />
          </div>
          {/* Other toolbox items can be added here */}
        </div>
      </div>
    </div>
  );
}