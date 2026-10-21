from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.colors import HexColor
from pypdf import PdfReader,PdfWriter
import pypdfium2 as pdfium
root=Path.cwd()
overlay=root/'tmp/pdfs/concise-overlay.pdf'
c=canvas.Canvas(str(overlay),pagesize=(612,792))
normal=ParagraphStyle('body',fontName='Helvetica',fontSize=9,leading=12,spaceAfter=5)
heading=ParagraphStyle('heading',fontName='Helvetica-Bold',fontSize=10,leading=13,textColor=HexColor('#25446b'))
y=435
def add(text,style=normal,gap=6):
 global y
 p=Paragraph(text,style);_,height=p.wrap(518,700);p.drawOn(c,47,y-height);y-=height+gap
add('SELECTED PROJECT',ParagraphStyle('section',fontName='Helvetica-Bold',fontSize=10,leading=13),8)
add('NoPressure Wear | E-commerce Application',heading,3)
add('Personal project | Backend &amp; Frontend Development',ParagraphStyle('sub',fontName='Helvetica',fontSize=9,leading=12,textColor=HexColor('#555555')),8)
for text in [
'Developed a Spring Boot backend and responsive React storefront and admin panel, covering product and variant management, inventory, carts, checkout, orders, coupons, and reporting.',
'Chose a modular monolith architecture to organize business capabilities within one deployable application and support easier future extraction into microservices.',
'Implemented REST APIs, PostgreSQL persistence and Flyway migrations, JWT/OAuth2 authentication, role-based authorization, Stripe payments and webhooks, and transactional email notifications.',
'Built reusable UI components, search and filtering, English/Serbian localization, and customer account flows; added automated backend tests, Docker Compose, and GitHub Actions CI.'
]:add('&#8226; '+text)
add('<b>Technologies:</b> Java, Spring Boot, Spring Security, JPA/Hibernate, PostgreSQL, Flyway, REST, JWT, OAuth2, Stripe, React, JavaScript, Vite, Tailwind CSS, Axios, TanStack Query, i18next, Maven, Docker, GitHub Actions, JUnit 5, Mockito, MockMvc.',gap=0)
c.save()
r=PdfReader(r'C:\milan\CV\ja\AWS & Kafka\MilanGataric_CV.pdf')
r.pages[1].merge_page(PdfReader(overlay).pages[0])
w=PdfWriter()
for p in r.pages:w.add_page(p)
w.add_metadata({'/Title':'Milan Gataric - CV - NoPressure Wear','/Author':'Milan Gataric'})
out=root/'output/pdf/MilanGataric_CV_NoPressureWear_Concise.pdf'
with out.open('wb') as f:w.write(f)
d=pdfium.PdfDocument(str(out));d[1].render(scale=1.5).to_pil().save(root/'tmp/pdfs/concise-page-2.png')
print(out);print('Pages:',len(d),'Content bottom:',y)
