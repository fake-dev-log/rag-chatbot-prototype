
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL, baseURL } from '../types/common';
import type { Document } from '../types/document';
import api from '../index';
import { useToastStore } from '@stores/toast.ts';

export function useDocumentList() {
  return useQuery<Document[], Error>({
    queryKey: ['documents', 'list'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_BASE_URL.documents}`);
      return response.data as Document[];
    },
  });
}

export function useUploadDocument() {
  const queryClient = useQueryClient();

  return useMutation<Document, Error, File>({
    onMutate: async (file) => {
      useToastStore.getState().addToast(`Uploading document '${file.name}'...`, 'info');
    },
    mutationFn: async (file) => {
      const formData = new FormData();
      formData.append('file', file);

      const response = await api.post(`${baseURL}${API_BASE_URL.documents}/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data as Document;
    },
    onSuccess: (data) => {
      useToastStore.getState().addToast(`Document '${data.name}' uploaded and indexing completed.`, 'success');
      return queryClient.invalidateQueries({ queryKey: ['documents', 'list'] });
    },
  });
}

export function useDeleteDocument() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    onMutate: async (documentId) => {
      useToastStore.getState().addToast(`Deleting document ID '${documentId}'...`, 'info');
    },
    mutationFn: async (documentId) => {
      await api.delete(`${baseURL}${API_BASE_URL.documents}/${documentId}`);
    },
    onSuccess: (_, documentId) => {
      useToastStore.getState().addToast(`Document ID '${documentId}' deleted and de-indexing completed.`, 'success');
      return queryClient.invalidateQueries({ queryKey: ['documents', 'list'] });
    },
  });
}
