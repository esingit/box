export default [
  {
    url: '/api/user/info',
    method: 'get',
    response: () => {
      return {
        code: 200,
        data: {
          name: 'ESin',
          role: 'admin'
        }
      }
    }
  }
]