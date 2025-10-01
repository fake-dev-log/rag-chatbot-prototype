
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { API_PATHS, baseURL } from '../types/common';
import type { Document } from '../types/document';
import api from '../index';
import { toast } from '@utils/toast.ts';
import { useEffect } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { fetchWithAuth } from '@apis/fetchWithAuth.ts';

const documentQueryKey = 'document';

export function useDocumentSse() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const abortController = new AbortController();

    fetchEventSource(`${baseURL}${API_PATHS.admin.documents.statusStream}`, {
      fetch: fetchWithAuth, // Use the custom fetch for authentication
      signal: abortController.signal,
      onmessage(event) {
        const updatedDocument = JSON.parse(event.data) as Document;

        queryClient.setQueryData<Document[]>([documentQueryKey, 'list'], (oldData) => {
          if (!oldData) return [updatedDocument];
          const docExists = oldData.some(doc => doc.id === updatedDocument.id);

          if (docExists) {
            return oldData.map(doc => doc.id === updatedDocument.id ? updatedDocument : doc);
          } else {
            return [updatedDocument, ...oldData];
          }
        });
      },
      onerror(err) {
        console.error("EventSource failed:", err);
        abortController.abort();
      }
    });

    return () => {
      abortController.abort();
    };
  }, [queryClient]);
}

export function useDocumentList() {
  return useQuery<Document[], Error>({
    queryKey: [documentQueryKey, 'list'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_PATHS.admin.documents.root}`);
      return response.data as Document[];
    },
  });
}

export function useUploadDocument() {
  return useMutation<Document, Error, { file: File, category: string }>({
    onMutate: async ({ file }) => {
      toast.info(`Uploading document '${file.name}'...`);
    },
    mutationFn: async ({ file, category }) => {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('category', category);

      const response = await api.post(`${baseURL}${API_PATHS.admin.documents.upload}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data as Document;
    },
    onSuccess: (data) => {
      toast.success(`Document '${data.name}' uploaded. Indexing has started.`);
      // Invalidation is now handled by the SSE event stream.
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
      await api.delete(`${baseURL}${API_PATHS.admin.documents.byId(documentId)}`);
    },
    onSuccess: (_, documentId) => {
      toast.success(`Document ID '${documentId}' deleted and de-indexing completed.`);
      return queryClient.invalidateQueries({ queryKey: [documentQueryKey] });
    },
  });
}

export function useDocumentCategories() {
  return useQuery<string[], Error>({
    queryKey: [documentQueryKey, 'categories'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_PATHS.admin.documents.categories}`);
      return response.data as string[];
    },
  });
}
