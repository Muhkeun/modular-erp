import { create } from 'zustand';
import { preferenceApi, type PreferenceCategory } from '../api/preferenceApi';

interface PreferenceState {
  preferences: Record<string, string>;
  loaded: boolean;
  load: () => Promise<void>;
  get: (category: PreferenceCategory, key: string, defaultValue?: string) => string;
  set: (category: PreferenceCategory, key: string, value: string) => Promise<void>;
}

const prefKey = (category: string, key: string) => `${category}:${key}`;

export const usePreferenceStore = create<PreferenceState>((set, get) => ({
  preferences: {},
  loaded: false,

  load: async () => {
    try {
      const items = await preferenceApi.getAll();
      const map: Record<string, string> = {};
      items.forEach((item) => {
        map[prefKey(item.category, item.key)] = item.value;
      });
      set({ preferences: map, loaded: true });
    } catch {
      set({ loaded: true });
    }
  },

  get: (category, key, defaultValue = '') => {
    return get().preferences[prefKey(category, key)] ?? defaultValue;
  },

  set: async (category, key, value) => {
    set((state) => ({
      preferences: { ...state.preferences, [prefKey(category, key)]: value },
    }));
    await preferenceApi.save(category, key, value);
  },
}));

/**
 * 특정 설정값을 사용하는 훅.
 */
export function usePreference(category: PreferenceCategory, key: string, defaultValue = '') {
  const store = usePreferenceStore();
  const value = store.get(category, key, defaultValue);
  const setValue = (v: string) => store.set(category, key, v);
  return [value, setValue] as const;
}
