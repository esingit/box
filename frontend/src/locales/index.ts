import { createI18n } from 'vue-i18n'
import zh from './lang/zh'
import en from './lang/en'

export const setupI18n = (app: any) => {
  const i18n = createI18n({
    legacy: false,
    locale: 'zh',
    messages: {
      zh,
      en
    }
  })
  app.use(i18n)
}