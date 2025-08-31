import { useState, type FormEvent } from 'react';
import { usePromptList, useCreatePrompt, useUpdatePrompt, useDeletePrompt, useApplyPrompt } from "@apis/hooks/prompt";
import type { PromptRequest, PromptResponse } from '@apis/types/prompt';

export default function PromptList() {
    const { data: prompts, isLoading, error } = usePromptList();
    const updatePrompt = useUpdatePrompt();
    const createPrompt = useCreatePrompt();
    const deletePrompt = useDeletePrompt();
    const applyPrompt = useApplyPrompt();

    const [selectedPrompt, setSelectedPrompt] = useState<PromptResponse | null>(null);
    const [isCreating, setIsCreating] = useState(false);

    const handleSelectPrompt = (prompt: PromptResponse) => {
        setSelectedPrompt(prompt);
        setIsCreating(false);
    };

    const handleCreateNew = () => {
        setSelectedPrompt(null);
        setIsCreating(true);
    };

    const handleDelete = (id: number) => {
        if (window.confirm(`Are you sure you want to delete prompt #${id}?`)) {
            deletePrompt.mutate(id, {
                onSuccess: () => {
                    setSelectedPrompt(null);
                    setIsCreating(false);
                }
            });
        }
    };

    if (isLoading) return <div>Loading prompts...</div>;
    if (error) return <div>Error: {error.message}</div>;

    return (
        <div className="flex h-full">
            {/* Prompt List Sidebar */}
            <div className="w-1/3 border-r overflow-y-auto">
                <div className="p-4 border-b flex justify-between items-center">
                    <h2 className="text-xl font-semibold">Prompt Templates</h2>
                    <button onClick={handleCreateNew} className="px-3 py-1 bg-green-500 text-white rounded-md hover:bg-green-600 text-sm">New</button>
                </div>
                <ul>
                    {prompts?.map(prompt => (
                        <li key={prompt.id}
                            onClick={() => handleSelectPrompt(prompt)}
                            className={`p-4 cursor-pointer hover:bg-gray-100 ${selectedPrompt?.id === prompt.id ? 'bg-blue-100' : ''}`}>
                            <div className="flex justify-between items-center">
                                <h3 className="font-semibold">{prompt.name}</h3>
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation(); // Prevent selecting the prompt when clicking the button
                                        applyPrompt.mutate(prompt.id);
                                    }}
                                    className="px-2 py-1 bg-blue-500 text-white rounded-md hover:bg-blue-600 text-xs"
                                >
                                    Apply
                                </button>
                            </div>
                            <p className="text-xs text-gray-500">Last updated: {new Date(prompt.updatedAt).toLocaleString()}</p>
                        </li>
                    ))}
                </ul>
            </div>

            {/* Prompt Editor */}
            <div className="w-2/3 p-6">
                {isCreating && <PromptForm prompt={{ name: '', templateContent: '' }} onSave={createPrompt.mutate} onCancel={() => setIsCreating(false)} isSaving={createPrompt.isPending} />}
                {selectedPrompt && <PromptForm prompt={selectedPrompt} onSave={(data) => updatePrompt.mutate({ id: selectedPrompt.id, data })} onCancel={() => setSelectedPrompt(null)} isSaving={updatePrompt.isPending} onDelete={() => handleDelete(selectedPrompt.id)} />}
                {!selectedPrompt && !isCreating && <div className="text-center text-gray-500">Select a prompt to edit or create a new one.</div>}
            </div>
        </div>
    );
}

interface PromptFormProps {
    prompt: Partial<PromptResponse> | PromptRequest;
    onSave: (data: PromptRequest) => void;
    onCancel: () => void;
    isSaving: boolean;
    onDelete?: () => void;
}

function PromptForm({ prompt, onSave, onCancel, isSaving, onDelete }: PromptFormProps) {
    const [name, setName] = useState(prompt.name);
    const [templateContent, setTemplateContent] = useState(prompt.templateContent);

    const handleSubmit = (e: FormEvent) => {
        e.preventDefault();
        if (name && templateContent) {
            onSave({ name, templateContent });
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700">Prompt Name</label>
                <input 
                    type="text" 
                    id="name" 
                    value={name}
                    onChange={e => setName(e.target.value)}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500"
                    required
                />
            </div>
            <div>
                <label htmlFor="templateContent" className="block text-sm font-medium text-gray-700">Template Content</label>
                <textarea 
                    id="templateContent"
                    value={templateContent}
                    onChange={e => setTemplateContent(e.target.value)}
                    rows={20}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 font-mono text-sm"
                    required
                />
            </div>
            <div className="flex justify-end space-x-4">
                {onDelete && <button type="button" onClick={onDelete} className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50" disabled={isSaving}>Delete</button>}
                <button type="button" onClick={onCancel} className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">Cancel</button>
                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50" disabled={isSaving}>
                    {isSaving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
        </form>
    );
}
