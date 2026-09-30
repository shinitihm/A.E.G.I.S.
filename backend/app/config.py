from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    comic_vine_api_key: str = ""
    comic_vine_base_url: str = "https://comicvine.gamespot.com/api/"
    cache_ttl_seconds: int = 6 * 60 * 60

    gemini_api_key: str = ""
    jarvis_model: str = "gemini-2.5-flash"


settings = Settings()
