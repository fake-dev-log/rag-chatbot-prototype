import {type ChangeEvent, useState, type FormEvent} from 'react';
import { useDocumentList, useUploadDocument, useDeleteDocument } from '@apis/hooks/document.ts';

function DocumentList() {
  const { data: documents, isLoading, error } = useDocumentList();
  const uploadDocument = useUploadDocument();
  const deleteDocument = useDeleteDocument();

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [category, setCategory] = useState("");

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
  };

  const handleDelete = (documentId: number) => {
    deleteDocument.mutate(documentId);
  };

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>An error occurred: {error.message}</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-semibold text-gray-800 dark:text-gray-300">Documents</h1>
        {/* New Upload Form */}
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
                <td className="py-3 px-6 text-center">{doc.type}</td>
                <td className="py-3 px-6 text-center">{(doc.size / 1024).toFixed(2)}</td>
                <td className="py-3 px-6 text-center">{doc.createdAt}</td>
                <td className="py-3 px-6 text-center">
                  <div className="flex item-center justify-center">
                    <button onClick={() => handleDelete(doc.id)} className="w-4 mr-2 transform hover:text-red-500 hover:scale-110">
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
    </div>
  );
}

export default DocumentList;
