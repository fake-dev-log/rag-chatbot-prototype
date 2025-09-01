import { useState } from 'react';
import { usePromptList, useCreatePrompt, useUpdatePrompt, useDeletePrompt, useApplyPrompt } from "@apis/hooks/prompt";
import type { PromptResponse } from '@apis/types/prompt';
import PromptForm from './PromptForm';
import ConfirmModal from '@components/ConfirmModal';

export default function PromptList() {
    const { data: prompts, isLoading, error } = usePromptList();
    const updatePrompt = useUpdatePrompt();
    const createPrompt = useCreatePrompt();
    const deletePrompt = useDeletePrompt();
    const applyPrompt = useApplyPrompt();

    const [selectedPrompt, setSelectedPrompt] = useState<PromptResponse | null>(null);
    const [isCreating, setIsCreating] = useState(false);
    const [isConfirmOpen, setIsConfirmOpen] = useState(false);
    const [promptToDelete, setPromptToDelete] = useState<number | null>(null);

    const handleSelectPrompt = (prompt: PromptResponse) => {
        setSelectedPrompt(prompt);
        setIsCreating(false);
    };

    const handleCreateNew = () => {
        setSelectedPrompt(null);
        setIsCreating(true);
    };

    const handleDeleteClick = (id: number) => {
        setPromptToDelete(id);
        setIsConfirmOpen(true);
    };

    const handleConfirmDelete = () => {
        if (promptToDelete) {
            deletePrompt.mutate(promptToDelete, {
                onSuccess: () => {
                    setSelectedPrompt(null);
                    setIsCreating(false);
                    setPromptToDelete(null);
                    setIsConfirmOpen(false);
                }
            });
        }
    };

    const handleCancelDelete = () => {
        setPromptToDelete(null);
        setIsConfirmOpen(false);
    };

    if (isLoading) return <div className="p-4">Loading prompts...</div>;
    if (error) return <div className="p-4 text-red-500">Error: {error.message}</div>;

    return (
        <>
            <div className="flex h-full bg-gray-50 dark:bg-gray-900 text-gray-800 dark:text-gray-200">
                {/* Prompt List Sidebar */}
                <div className="w-1/3 border-r border-gray-200 dark:border-gray-700 overflow-y-auto">
                    <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
                        <h2 className="text-xl font-semibold">Prompt Templates</h2>
                        <button onClick={handleCreateNew} className="px-3 py-1 bg-green-500 text-white rounded-md hover:bg-green-600 text-sm">New</button>
                    </div>
                    <ul>
                        {prompts?.map(prompt => (
                            <li key={prompt.id}
                                onClick={() => handleSelectPrompt(prompt)}
                                className={`p-4 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 ${selectedPrompt?.id === prompt.id ? 'bg-blue-100 dark:bg-gray-700' : ''}`}>
                                <div className="flex justify-between items-center">
                                    <h3 className="font-semibold">{prompt.name}</h3>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            applyPrompt.mutate(prompt.id);
                                        }}
                                        className="px-2 py-1 bg-blue-500 text-white rounded-md hover:bg-blue-600 text-xs"
                                    >
                                        Apply
                                    </button>
                                </div>
                                <p className="text-xs text-gray-500 dark:text-gray-400">Last updated: {new Date(prompt.updatedAt).toLocaleString()}</p>
                            </li>
                        ))}
                    </ul>
                </div>

                {/* Prompt Editor */}
                <div className="w-2/3 p-6">
                    {isCreating && <PromptForm prompt={{ name: '', templateContent: '' }} onSave={(data) => createPrompt.mutate(data, { onSuccess: () => setIsCreating(false) })} onCancel={() => setIsCreating(false)} isSaving={createPrompt.isPending} />}
                    {selectedPrompt && <PromptForm prompt={selectedPrompt} onSave={(data) => updatePrompt.mutate({ id: selectedPrompt.id, data })} onCancel={() => setSelectedPrompt(null)} isSaving={updatePrompt.isPending} onDelete={() => handleDeleteClick(selectedPrompt.id)} />}
                    {!selectedPrompt && !isCreating && <div className="text-center text-gray-500 dark:text-gray-400">Select a prompt to edit or create a new one.</div>}
                </div>
            </div>
            <ConfirmModal
                isOpen={isConfirmOpen}
                onClose={handleCancelDelete}
                onConfirm={handleConfirmDelete}
                title="Delete Prompt"
                message={`Are you sure you want to delete prompt #${promptToDelete}? This action cannot be undone.`}
            />
        </>
    );
}
