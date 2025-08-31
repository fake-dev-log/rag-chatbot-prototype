interface PromptRequest {
        name: string;
        templateContent: string;
}
interface PromptResponse {
        id: number;
        name: string;
        templateContent: string;
        createdAt: string; // Assuming ISO date string
        updatedAt: string; // Assuming ISO date string
}

export type {
    PromptRequest,
    PromptResponse
}
