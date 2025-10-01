export const baseURL = import.meta.env.VITE_API_BASE_URL;

// 1. Centralize resource names as readonly constants
const API_RESOURCES = {
  AUTH: 'auth',
  ADMIN: 'admin',
  CHATS: 'chats',
  DOCUMENTS: 'documents',
  PROMPTS: 'prompts',
} as const;

// 2. Build prefixes from resource names
const API_PREFIX = {
  auth: `/${API_RESOURCES.AUTH}`,
  admin: `/${API_RESOURCES.ADMIN}`,
  chats: `/${API_RESOURCES.CHATS}`,
} as const;

// 3. Define base paths for each resource to minimize repetition
const adminDocumentsPath = `${API_PREFIX.admin}/${API_RESOURCES.DOCUMENTS}`;
const adminPromptsPath = `${API_PREFIX.admin}/${API_RESOURCES.PROMPTS}`;

// 4. Construct the final, structured API paths object
export const API_PATHS = {
  auth: {
    root: API_PREFIX.auth,
    signIn: `${API_PREFIX.auth}/sign-in`,
    signOut: `${API_PREFIX.auth}/sign-out`,
    refresh: `${API_PREFIX.auth}/refresh`,
  },
  chats: {
    root: API_PREFIX.chats,
    byId: (chatId: number) => `${API_PREFIX.chats}/${chatId}`,
    messages: (chatId: number) => `${API_PREFIX.chats}/${chatId}/message`,
  },
  admin: {
    documents: {
      root: adminDocumentsPath,
      byId: (docId: number) => `${adminDocumentsPath}/${docId}`,
      upload: `${adminDocumentsPath}/upload`,
      categories: `${adminDocumentsPath}/categories`,
      statusStream: `${adminDocumentsPath}/status-stream`,
    },
    prompts: {
      root: adminPromptsPath,
      byId: (promptId: number) => `${adminPromptsPath}/${promptId}`,
      apply: (promptId: number) => `${adminPromptsPath}/${promptId}/apply`,
    }
  }
};
