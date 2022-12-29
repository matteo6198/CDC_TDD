CREATE TABLE IF NOT EXISTS "CDC"."SCRAPED_DATA"
(
    "ID" bigint NOT NULL,
    website text COLLATE pg_catalog."default" NOT NULL,
    title text COLLATE pg_catalog."default" NOT NULL,
    date_article timestamp with time zone,
    link text COLLATE pg_catalog."default" NOT NULL,
    body text COLLATE pg_catalog."default",
    image_url text COLLATE pg_catalog."default",
    CONSTRAINT "SCRAPED_DATA_pkey" PRIMARY KEY ("ID")
)