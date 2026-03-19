import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { usePreference } from '../../../shared/hooks/usePreference';
import PageHeader from '../../../shared/components/PageHeader';

const DATE_FORMATS = ['YYYY-MM-DD', 'DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY.MM.DD'];
const NUMBER_FORMATS = [
  { label: '1,234.56', groupSep: ',', decimalSep: '.' },
  { label: '1.234,56', groupSep: '.', decimalSep: ',' },
  { label: '1 234.56', groupSep: ' ', decimalSep: '.' },
];

export default function SettingsPage() {
  const { t, i18n } = useTranslation();
  const [activeTab, setActiveTab] = useState<'display' | 'format' | 'default'>('display');

  // Display settings
  const [theme, setTheme] = usePreference('DISPLAY', 'theme', 'light');
  // Format settings
  const [dateFormat, setDateFormat] = usePreference('FORMAT', 'dateFormat', 'YYYY-MM-DD');
  const [numberGroupSep, setNumberGroupSep] = usePreference('FORMAT', 'numberGroupSep', ',');
  const [numberDecimalSep, setNumberDecimalSep] = usePreference('FORMAT', 'numberDecimalSep', '.');

  // Default settings
  const [defaultLang, setDefaultLang] = usePreference('DEFAULT', 'language', i18n.language);

  const tabs = [
    { key: 'display' as const, label: t('settings.display', '화면 설정') },
    { key: 'format' as const, label: t('settings.format', '표시 형식') },
    { key: 'default' as const, label: t('settings.defaults', '기본값') },
  ];

  return (
    <div>
      <PageHeader title={t('settings.title', '개인 설정')} />

      {/* Tabs */}
      <div className="flex gap-1 mb-6 border-b border-slate-200">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2.5 text-sm font-medium transition-colors border-b-2 -mb-px ${
              activeTab === tab.key
                ? 'border-brand-600 text-brand-700'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Display Tab */}
      {activeTab === 'display' && (
        <div className="space-y-6 max-w-lg">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              {t('settings.theme', '테마')}
            </label>
            <div className="flex gap-3">
              {['light', 'dark'].map((v) => (
                <button
                  key={v}
                  onClick={() => setTheme(v)}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                    theme === v
                      ? 'bg-brand-600 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {v === 'light' ? '☀️ Light' : '🌙 Dark'}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Format Tab */}
      {activeTab === 'format' && (
        <div className="space-y-6 max-w-lg">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              {t('settings.dateFormat', '날짜 형식')}
            </label>
            <select
              value={dateFormat}
              onChange={(e) => setDateFormat(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm"
            >
              {DATE_FORMATS.map((f) => (
                <option key={f} value={f}>{f}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              {t('settings.numberFormat', '숫자 형식')}
            </label>
            <div className="flex gap-3">
              {NUMBER_FORMATS.map((nf) => (
                <button
                  key={nf.label}
                  onClick={() => {
                    setNumberGroupSep(nf.groupSep);
                    setNumberDecimalSep(nf.decimalSep);
                  }}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                    numberGroupSep === nf.groupSep && numberDecimalSep === nf.decimalSep
                      ? 'bg-brand-600 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {nf.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Default Tab */}
      {activeTab === 'default' && (
        <div className="space-y-6 max-w-lg">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              {t('settings.language', '언어')}
            </label>
            <div className="flex gap-3">
              {[{ code: 'ko', label: '한국어' }, { code: 'en', label: 'English' }].map((lang) => (
                <button
                  key={lang.code}
                  onClick={() => {
                    setDefaultLang(lang.code);
                    i18n.changeLanguage(lang.code);
                    localStorage.setItem('locale', lang.code);
                  }}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition ${
                    defaultLang === lang.code
                      ? 'bg-brand-600 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {lang.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
