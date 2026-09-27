import psycopg2
try:
    conn = psycopg2.connect(dbname='dormigo', user='postgres', password='root', host='localhost', port='5432')
    cur = conn.cursor()
    cur.execute("SELECT column_name, is_nullable, column_default FROM information_schema.columns WHERE table_name = 'messages';")
    for row in cur.fetchall():
        print(row)
except Exception as e:
    print(e)
