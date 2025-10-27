# Set the CRAN mirror
chooseCRANmirror(graphics = FALSE, ind = 1)

if (!require("pacman")) install.packages("pacman")
pacman::p_load(dplyr, readr, stringr)

mara <- read_tsv('data/mara.txt')
marc <- read_tsv('data/marc.txt')
mbew <- read_tsv('data/mbew.txt')

mara %>% 
  filter(!is.na(BISMT) & BISMT != "") %>%
  inner_join(marc) %>%
  inner_join(mbew) %>%
  mutate(PRICE = str_trim(format(round(VERPR * ifelse(MEINS == 'G', 1000, 1) / PEINH, digits = 3), decimal.mark = ','))) %>%
  mutate(SITE = 'MSSC', CURR = 'CHF') %>%
  mutate(CODEF = '', DESF = '', DESE = '', UOM = '', STAT = '', DESFO = '') %>%
  select(BISMT, PRICE, CODEF, SITE, DESF, DESE, UOM, STAT, DESFO, CURR) %>%
  write_excel_csv2('data/final_output.csv', quote = "none", col_names = F, eol="\r\n")

mara %>%
  write_excel_csv2('data/codes_article.csv', eol="\r\n")
