import { type FormEvent, useRef, useEffect, useState } from 'react';
import { Disclosure, DisclosureButton, DisclosurePanel } from '@headlessui/react';
import { ChevronUpIcon, ChevronDownIcon } from 'lucide-react';
import { Spinner } from '@components/Spinner';
import type {Message} from "@apis/types/chat.ts";
import ReactMarkdown from 'react-markdown';
import remarkGfm from "remark-gfm";
import ChatInput from "@components/ChatInput.tsx";

interface ChatWindowProps {
  messages: Message[];
  isStreaming: boolean;
  onSubmit: (query: string) => void;
  categories: string[];
  selectedCategory: string;
  setSelectedCategory: (category: string) => void;
}

export default function ChatWindow({ messages, isStreaming, onSubmit, categories, selectedCategory, setSelectedCategory }: ChatWindowProps) {
  const [input, setInput] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!input.trim()) return;
    onSubmit(input);
    setInput('');
  }

  // Auto-scroll
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages]);

  return (
    <div className="flex flex-col h-full w-full">
      {/* Message area */}
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto p-4 flex flex-col space-y-4"
      >
        {messages.map((msg, idx) => {
          const isUser = msg.from === 'USER';
          return (
            <div key={idx} className="max-w-3xl mx-auto w-full">
              {isUser ? (
                <div>
                  <div className="flex-box px-4 py-2 rounded-lg break-words whitespace-pre-wrap justify-self-end bg-primary-light text-surface-light dark:bg-primary-dark dark:text-surface-dark transition-colors duration-300">
                    {msg.text}
                  </div>
                  {msg.createdAt && <div className="flex-box justify-self-end pt-1.5 text-xs text-text-light dark:text-text-dark transition-colors duration-300">{new Date(msg.createdAt).toLocaleTimeString()}</div>}
                </div>
              ) : msg.text ? (
                <div>
                  <div className="markdown-table flex-box px-4 py-4 rounded-lg break-words whitespace-pre-wrap justify-self-start bg-surface-light text-text-light dark:bg-surface-dark dark:text-text-dark transition-colors duration-300">
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.text}</ReactMarkdown>
                  </div>
                  {msg.createdAt && <div className="flex-box justify-self-end pt-1.5 text-xs text-text-light dark:text-text-dark transition-colors duration-300">{new Date(msg.createdAt).toLocaleTimeString()}</div>}
                                  </div>
                                ) : isStreaming ? (                <div className="flex-box px-4 py-2 rounded-lg break-words whitespace-pre-wrap justify-self-start bg-surface-light text-text-light dark:bg-surface-dark dark:text-text-dark transition-colors duration-300">
                  <div className="flex items-center space-x-2"><Spinner /><span>Generating answer...</span></div>
                </div>
              ) : null}

              {!isUser && msg.text && (msg.sources?.length ?? 0) > 0 && (
                <Disclosure>
                  {({ open }) => (
                    <div className="max-w-3xl mx-auto w-full">
                      <DisclosureButton className="mt-2 flex items-center text-sm text-secondary-light dark:text-secondary-dark transition-colors duration-300">
                        {open ? <ChevronUpIcon className="w-4 h-4" /> : <ChevronDownIcon className="w-4 h-4" />}
                        <span className="ml-1">Sources</span>
                      </DisclosureButton>
                      <DisclosurePanel className="mt-2 ml-6 space-y-4 p-4 rounded-md text-sm bg-surface-light text-text-light dark:bg-surface-dark dark:text-text-dark ring-1 ring-secondary-light dark:ring-secondary-dark shadow-sm transition-colors duration-300">
                        {msg.sources?.map((src, i) => (
                          <div key={i} className="pb-3 border-b border-secondary-light dark:border-secondary-dark transition-colors duration-300">
                            <div className="font-semibold">{src.fileName} (page {src.pageNumber})</div>
                            {src.title && src.title !== "N/A" && <div className="italic text-text-light dark:text-text-dark">{src.title}</div>}
                            <div>{src.snippet}</div>
                          </div>
                        ))}
                      </DisclosurePanel>
                    </div>
                  )}
                </Disclosure>
              )}
            </div>
          );
        })}
      </div>

      {/* Reusable Chat Input Component */}
      <ChatInput
        query={input}
        setQuery={setInput}
        category={selectedCategory}
        setCategory={setSelectedCategory}
        categories={categories}
        isStreaming={isStreaming}
        onSubmit={handleSubmit}
      />
    </div>
  )
}