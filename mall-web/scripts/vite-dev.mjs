import { createServer } from 'vite'

const server = await createServer({
  configFile: './vite.config.ts',
  server: {
    host: '0.0.0.0'
  }
})

await server.listen()
server.printUrls()

setInterval(() => {
  // Keep the process alive when it is launched without an interactive terminal.
}, 60_000)
