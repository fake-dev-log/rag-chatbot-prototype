import {type ChangeEvent, useState, type FormEvent, useRef} from 'react';
import { useDocumentList, useUploadDocument, useDeleteDocument, useDocumentSse } from '@apis/hooks/document.ts';
import type { IndexingStatus } from '@apis/types/document.ts';
import ConfirmModal from '@components/ConfirmModal.tsx';

// Helper component to display status with appropriate icons and colors
const StatusBadge = ({ status }: { status: IndexingStatus }) => {
  switch (status) {
    case 'PENDING':
      return (
        <div className="flex items-center justify-center text-yellow-500">
          <svg className="animate-spin h-4 w-4 mr-1.5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Pending
        </div>
      );
    case 'SUCCESS':
      return (
        <div className="flex items-center justify-center text-green-500">
          <svg className="h-4 w-4 mr-1.5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
          Success
        </div>
      );
    case 'FAILURE':
      return (
        <div className="flex items-center justify-center text-red-500">
           <svg className="h-4 w-4 mr-1.5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
          Failure
        </div>
      );
    default:
      return <div className="text-gray-500">Unknown</div>;
  }
};

function DocumentList() {
  const { data: documents, isLoading, error } = useDocumentList();
  const uploadDocument = useUploadDocument();
  const { mutate: deleteDocument } = useDeleteDocument();

  // Custom hook to handle SSE connection and real-time updates
  useDocumentSse();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [category, setCategory] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  // State for the delete confirmation modal
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedDocId, setSelectedDocId] = useState<number | null>(null);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    if (event.target.files) {
      setSelectedFile(event.target.files[0]);
    }
  };

  const handleUploadSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFile || !category.trim()) {
      alert("Please select a file and enter a category.");
      return;
    }
    uploadDocument.mutate({ file: selectedFile, category: category.trim() });
    setSelectedFile(null);
    setCategory("");
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleOpenDeleteModal = (docId: number) => {
    setSelectedDocId(docId);
    setIsDeleteModalOpen(true);
  };

  const handleCloseDeleteModal = () => {
    setIsDeleteModalOpen(false);
    setSelectedDocId(null);
  };

  const handleConfirmDelete = () => {
    if (selectedDocId) {
      deleteDocument(selectedDocId);
    }
    handleCloseDeleteModal();
  };

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>An error occurred: {error.message}</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-semibold text-gray-800 dark:text-gray-300">Documents</h1>
        <form onSubmit={handleUploadSubmit} className="flex items-center space-x-2">
          <input
            type="text"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="Enter category"
            className="px-4 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600"
            required
          />
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
            required
          />
          <button type="submit" className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600" disabled={uploadDocument.isPending}>
            {uploadDocument.isPending ? 'Uploading...' : 'Upload'}
          </button>
        </form>
      </div>
      <div className="bg-white shadow-md rounded my-6">
        <table className="min-w-max w-full table-auto">
          <thead>
            <tr className="bg-gray-200 text-gray-600 uppercase text-sm leading-normal">
              <th className="py-3 px-6 text-left">ID</th>
              <th className="py-3 px-6 text-left">Name</th>
              <th className="py-3 px-6 text-left">Category</th>
              <th className="py-3 px-6 text-center">Status</th>
              <th className="py-3 px-6 text-center">Type</th>
              <th className="py-3 px-6 text-center">Size (KB)</th>
              <th className="py-3 px-6 text-center">Created At</th>
              <th className="py-3 px-6 text-center">Actions</th>
            </tr>
          </thead>
          <tbody className="text-gray-600 text-sm font-light">
            {documents?.map(doc => (
              <tr key={doc.id} className="border-b border-gray-200 hover:bg-gray-100">
                <td className="py-3 px-6 text-left whitespace-nowrap">{doc.id}</td>
                <td className="py-3 px-6 text-left">{doc.name}</td>
                <td className="py-3 px-6 text-left">{doc.category}</td>
                <td className="py-3 px-6 text-center"><StatusBadge status={doc.status} /></td>
                <td className="py-3 px-6 text-center">{doc.type}</td>
                <td className="py-3 px-6 text-center">{(doc.size / 1024).toFixed(2)}</td>
                <td className="py-3 px-6 text-center">{doc.createdAt}</td>
                <td className="py-3 px-6 text-center">
                  <div className="flex item-center justify-center">
                    <button onClick={() => handleOpenDeleteModal(doc.id)} className="w-4 mr-2 transform hover:text-red-500 hover:scale-110">
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={handleCloseDeleteModal}
        onConfirm={handleConfirmDelete}
        title="Delete Document"
        message="Are you sure you want to delete this document? This action cannot be undone."
      />
    </div>
  );
}

export default DocumentList;