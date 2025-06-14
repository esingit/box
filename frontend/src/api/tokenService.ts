const TOKEN_KEY = 'app_token'

export const tokenService = {
    getToken(): string | null {
        return localStorage.getItem(TOKEN_KEY)
    },
    setToken(token: string): void {
        localStorage.setItem(TOKEN_KEY, token)
    },
    removeToken(): void {
        localStorage.removeItem(TOKEN_KEY)
    }
}
