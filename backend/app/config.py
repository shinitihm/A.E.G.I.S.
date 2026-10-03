from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    comic_vine_api_key: str = ""
    comic_vine_base_url: str = "https://comicvine.gamespot.com/api/"
    cache_ttl_seconds: int = 6 * 60 * 60

    gemini_api_key: str = ""
    jarvis_model: str = "gemini-2.5-flash"

    # Manchetes do briefing: qualquer feed RSS 2.0 serve (a ordem do feed é a ordem das manchetes).
    news_feed_url: str = "https://news.google.com/rss?hl=pt-BR&gl=BR&ceid=BR:pt-419"


settings = Settings()
