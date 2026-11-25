# Platform integration status (reviewed 2026-11-25)

This document separates verified integration options from assumptions. A platform appearing in the Sala do Futuro portal does not mean it exposes an API for student-side task automation.

| Platform | Verified public integration | Jarvis EDU approach |
|---|---|---|
| Alura | Public course/catalog JSON endpoints exist. Alura for Business APIs require an organization-issued token and are aimed at corporate administration/content/progress data. | Use public course metadata where appropriate. Do not assume an individual student has an API for submitting coursework. |
| Sala do Futuro | Official documentation describes it as an access hub connecting students to services such as Alura, Matific and Khan Academy. No public student-task API was found in the official material reviewed. | User-authorized selected-text capture; no credential handling or hidden browser automation. |
| Khan Academy | The old public API repository documents endpoint removals in 2020 and is archived. | Do not build against the retired API. Use user-provided question text and approved links. |
| Matific | Public help material reviewed covers school network/service configuration, not a student assignment API. | User-authorized selected-text capture until an official API and permission model are confirmed. |
| Other platforms | Must be evaluated individually. | Add only documented, authorized APIs or explicit user-driven capture. |

## Official references reviewed
- Alura public API overview: https://suporte.alura.com.br/como-funciona-a-api-p%C3%BAblica-da-alura
- Alura for Business API docs: https://www.alura.com.br/empresas/docs/category-api
- São Paulo education department, Sala do Futuro ecosystem guide: https://atendimento.educacao.sp.gov.br/knowledgebase/article/SED-08324/pt-br
- Khan Academy legacy API archive and removal notice: https://github.com/Khan/khan-api
- Matific network access help: https://help.matific.com/en/articles/15486131-configure-network-access-for-matific

## Important
No credentials, student records, session cookies, or passwords should be copied into Jarvis EDU. The user must remain signed in directly on the official platform. The extension only imports text that the user selects and explicitly submits.
