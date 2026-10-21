from pathlib import Path
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.colors import HexColor
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from pypdf import PdfReader,PdfWriter
import pypdfium2 as pdfium
root=Path.cwd()
pdfmetrics.registerFont(TTFont('ArialCV','C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('ArialCVBold','C:/Windows/Fonts/arialbd.ttf'))
source=PdfReader(r'C:\milan\CV\ja\AWS & Kafka\MilanGataric_CV.pdf')
w,h=map(float,source.pages[0].mediabox[2:])
styles={
 'section':ParagraphStyle('section',fontName='ArialCVBold',fontSize=11,textColor=HexColor('#233950'),spaceAfter=10),
 'title':ParagraphStyle('title',fontName='ArialCVBold',fontSize=11.5,leading=15,spaceAfter=3),
 'sub':ParagraphStyle('sub',fontName='ArialCV',fontSize=9,leading=12,textColor=HexColor('#526070'),spaceAfter=9),
 'body':ParagraphStyle('body',fontName='ArialCV',fontSize=9.3,leading=13,spaceAfter=6),
 'bullet':ParagraphStyle('bullet',fontName='ArialCV',fontSize=9.1,leading=12.7,leftIndent=10,firstLineIndent=-9,spaceAfter=5),
 'tech':ParagraphStyle('tech',fontName='ArialCV',fontSize=8.6,leading=12,spaceAfter=5)
}
story=[]
def p(text,style='body'): story.append(Paragraph(text,styles[style]))
p('SELECTED PROJECT | BACKEND &amp; FRONTEND DEVELOPMENT','section')
p('NoPressure Wear | E-commerce Web Application','title')
p('Personal project | Java / Spring Boot backend and React storefront &amp; administration','sub')
p('Designed and developed an e-commerce application covering the customer shopping journey and store operations, from product discovery and checkout to inventory, order management, content administration, and business reporting.')
p('<b>Architecture &amp; backend engineering</b>')
items=[
'Chose a modular monolith architecture to organize business capabilities within a single deployable backend, with the goal of simplifying future extraction into microservices as the application grows. Separated responsibilities across controllers, services, repositories, DTOs, and persistence entities.',
'Built REST APIs with Spring Boot, Spring Data JPA/Hibernate, PostgreSQL, and Flyway migrations; implemented request validation, centralized exception handling, pagination, sorting, native SQL filtering, and soft deletion to preserve order history.',
'Implemented product and category management with nested categories, dynamic attributes, image/video galleries, SKU-based color variant linking, size and color filtering, and stock tracking per variant.',
'Implemented registration, login, email verification, password reset, JWT authentication, OAuth2 social login, and role-based authorization for ADMIN, EMPLOYEE, and CUSTOMER. Separated stateless API security from OAuth2 login and enforced service-level access rules.',
'Developed carts, guest and registered checkout, address management, coupon validation, delivery fees, order lifecycle tracking, and purchase-time price snapshots. Integrated Stripe PaymentIntents, saved payment methods, webhook signature verification, and payment status updates alongside cash on delivery.',
'Built transactional HTML emails, order status notifications, scheduled email retries, abandoned-cart reminders, and low-stock notifications. Added customer reviews, favorites, store locations, and configurable legal content.',
'Implemented administrative dashboards and revenue, order, product, and customer reports, including PDF and Excel exports; added configurable store settings, promotions, banners, popups, and catalog filters.'
]
for x in items:p('- '+x,'bullet')
p('<b>Frontend &amp; user experience</b>')
for x in [
'Built a responsive React storefront and admin panel with Vite and Tailwind CSS: product search and filtering, product galleries and variants, favorites, cart, Stripe checkout, order tracking, profile and address management, and reusable administration components.',
'Integrated REST APIs through Axios with JWT handling; used React Router, TanStack Query, React Hook Form, reusable hooks and context, toast notifications, loading states, and English/Serbian localization with i18next.'
]:p('- '+x,'bullet')
p('<b>Quality, integrations &amp; delivery</b>')
for x in [
'Wrote JUnit 5, Mockito, MockMvc, and integration tests for business logic, controllers, authorization, token invalidation, variant stock, filtering, and reporting. Configured GitHub Actions CI with PostgreSQL and Maven verification.',
'Containerized the backend, PostgreSQL, and nginx-served frontend with Docker Compose. Integrated Cloudinary/local media storage, background removal, Sentry monitoring, Spring Boot Actuator, and Caffeine caching.'
]:p('- '+x,'bullet')
p('<b>Technologies:</b> Java, Spring Boot, Spring Security, JWT, OAuth2, Spring Data JPA, Hibernate, PostgreSQL, Flyway, Maven, Lombok, WebClient, Stripe, JavaMail, React, JavaScript, Vite, Tailwind CSS, Axios, React Router, TanStack Query, React Hook Form, i18next, Docker, nginx, GitHub Actions, JUnit 5, Mockito, MockMvc, Cloudinary, Sentry, Caffeine, Apache POI, OpenPDF.','tech')
def footer(c,d):
 c.setFont('ArialCV',8);c.setFillColor(HexColor('#687585'));c.drawString(40,h-27,'Milan Gataric | CV');c.drawRightString(w-40,24,'3')
project=root/'tmp/pdfs/project.pdf'
SimpleDocTemplate(str(project),pagesize=(w,h),leftMargin=40,rightMargin=40,topMargin=48,bottomMargin=38).build(story,onFirstPage=footer,onLaterPages=footer)
r=PdfReader(project)
assert len(r.pages)==1, f'Project overflow: {len(r.pages)} pages'
writer=PdfWriter()
for page in source.pages:writer.add_page(page)
writer.add_page(r.pages[0])
writer.add_metadata({'/Title':'Milan Gataric - CV - NoPressure Wear','/Author':'Milan Gataric'})
out=root/'output/pdf/MilanGataric_CV_NoPressureWear.pdf'
with out.open('wb') as f:writer.write(f)
doc=pdfium.PdfDocument(str(out))
for i in range(len(doc)):
 doc[i].render(scale=1.4).to_pil().save(root/f'tmp/pdfs/cv-page-{i+1}.png')
print(out);print('Pages:',len(doc))
