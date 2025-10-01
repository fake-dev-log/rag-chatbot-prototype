import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { API_PATHS, baseURL } from '../types/common';
import type { PromptRequest, PromptResponse } from '@apis/types/prompt';
import api from '../index';
import { toast } from '@utils/toast.ts';

const promptQueryKey = 'prompt';

export function usePromptList() {
  return useQuery<PromptResponse[], Error>({
    queryKey: [promptQueryKey, 'list'],
    queryFn: async () => {
      const response = await api.get(`${baseURL}${API_PATHS.admin.prompts.root}`);
      return response.data;
    },
  });
}

export function useCreatePrompt() {
  const queryClient = useQueryClient();
  return useMutation<PromptResponse, Error, PromptRequest>({
    mutationFn: async (prompt) => {
      const response = await api.post(`${baseURL}${API_PATHS.admin.prompts.root}`, prompt);
      return response.data;
    },
    onSuccess: () => {
      return queryClient.invalidateQueries({ queryKey: [promptQueryKey] });
    },
  });
}

export function useUpdatePrompt() {
  const queryClient = useQueryClient();
  return useMutation<PromptResponse, Error, { id: number; data: PromptRequest }>({
    mutationFn: async ({ id, data }) => {
      const response = await api.put(`${baseURL}${API_PATHS.admin.prompts.byId(id)}`, data);
      return response.data;
    },
    onSuccess: () => {
      return queryClient.invalidateQueries({ queryKey: [promptQueryKey] });
    },
  });
}

export function useDeletePrompt() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>{
      const response = await api.delete(`${baseURL}${API_PATHS.admin.prompts.byId(id)}`);
      return response;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [promptQueryKey] });
      toast.success("Prompt deleted successfully!");
    },
    onError: (error) => {
      toast.error(`Failed to delete prompt: ${error.message}`);
    },
  });
}

export function useApplyPrompt() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number) =>{
      const response = await api.post(`${baseURL}${API_PATHS.admin.prompts.apply(id)}`)
      return response;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [promptQueryKey] });
      toast.success("Prompt applied successfully!");
    },
    onError: (error) => {
      toast.error(`Failed to apply prompt: ${error.message}`);
    },
  });
}
