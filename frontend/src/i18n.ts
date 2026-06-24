import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

import en from './locales/en.json'
import hu from './locales/hu.json'

// A nyelvválasztást localStorage-ban őrizzük (kulcs: "nexa.lang"),
// így az oldalújratöltés után is megmarad. Alapértelmezett: magyar.
export const SUPPORTED_LANGUAGES = ['hu', 'en'] as const
export type Language = (typeof SUPPORTED_LANGUAGES)[number]

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      hu: { translation: hu },
    },
    fallbackLng: 'hu',
    supportedLngs: SUPPORTED_LANGUAGES,
    // A böngésző "en-US"/"en-GB" stb. kódját a sima "en"-re képezzük le,
    // hogy a régió-variánsok is angolt kapjanak (ne essenek vissza a fallbackre).
    load: 'languageOnly',
    nonExplicitSupportedLngs: true,
    interpolation: { escapeValue: false },
    detection: {
      // Sorrend: megosztható ?lng=… link → mentett választás → böngésző nyelve.
      order: ['querystring', 'localStorage', 'navigator'],
      lookupQuerystring: 'lng',
      lookupLocalStorage: 'nexa.lang',
      caches: ['localStorage'],
    },
  })

export default i18n
