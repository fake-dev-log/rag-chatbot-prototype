export type IndexingStatus = 'PENDING' | 'SUCCESS' | 'FAILURE';

export interface Document {
  id: number;
  name: string;
  type: string;
  size: number;
  category?: string;
  createdAt: string;
  status: IndexingStatus;
}
