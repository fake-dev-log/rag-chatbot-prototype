
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL, baseURL } from '../types/common';
import type { Document } from '../types/document';
import api from '../index';
import { toast } from '@utils/toast.ts';

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

  return useMutation<Document, Error, { file: File, category: string }>({
    onMutate: async ({ file }) => {
      toast.info(`Uploading document '${file.name}'...`);
    },
    mutationFn: async ({ file, category }) => {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('category', category);

      const response = await api.post(`${baseURL}${API_BASE_URL.documents}/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data as Document;
    },
    onSuccess: (data) => {
      toast.success(`Document '${data.name}' uploaded and indexing completed.`);
      queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });
}

export function useDeleteDocument() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number>({
    onMutate: async (documentId) => {
      toast.info(`Deleting document ID '${documentId}'...`);
    },
    mutationFn: async (documentId) => {
      await api.delete(`${baseURL}${API_BASE_URL.documents}/${documentId}`);
    },
    onSuccess: (_, documentId) => {
      toast.success(`Document ID '${documentId}' deleted and de-indexing completed.`);
      return queryClient.invalidateQueries({ queryKey: ['documents'] });
    },
  });
}

export function useDocumentCategories() {
  return useQuery<string[], Error>({
    queryKey: ['documents', 'categories'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_BASE_URL.documents}/categories`);
      return response.data as string[];
    },
  });
}
