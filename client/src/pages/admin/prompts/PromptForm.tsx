import { useState, type FormEvent, useEffect } from 'react';
import type { PromptRequest, PromptResponse } from '@apis/types/prompt';

interface PromptFormProps {
    prompt: Partial<PromptResponse> | PromptRequest;
    onSave: (data: PromptRequest) => void;
    onCancel: () => void;
    isSaving: boolean;
    onDelete?: () => void;
}

export default function PromptForm({ prompt, onSave, onCancel, isSaving, onDelete }: PromptFormProps) {
    const [name, setName] = useState(prompt.name);
    const [templateContent, setTemplateContent] = useState(prompt.templateContent);

    useEffect(() => {
        setName(prompt.name);
        setTemplateContent(prompt.templateContent);
    }, [prompt]);

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        if (name && templateContent) {
            onSave({ name, templateContent });
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-6">
            <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Prompt Name</label>
                <input
                    type="text"
                    id="name"
                    value={name || ''}
                    onChange={e => setName(e.target.value)}
                    className="mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 text-gray-900 dark:text-gray-100"
                    required
                />
            </div>
            <div>
                <label htmlFor="templateContent" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Template Content</label>
                <textarea
                    id="templateContent"
                    value={templateContent || ''}
                    onChange={e => setTemplateContent(e.target.value)}
                    rows={20}
                    className="mt-1 block w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 font-mono text-sm text-gray-900 dark:text-gray-100"
                    required
                />
            </div>
            <div className="flex justify-end space-x-4">
                {onDelete && <button type="button" onClick={onDelete} className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50" disabled={isSaving}>Delete</button>}
                <button type="button" onClick={onCancel} className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm text-sm font-medium text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 hover:bg-gray-50 dark:hover:bg-gray-600">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50" disabled={isSaving}>
                    {isSaving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
        </form>
    );
}
