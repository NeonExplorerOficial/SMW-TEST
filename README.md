# SMW Editor (Android) — v0.1

Editor de niveles de Super Mario World para Android. Este es el
esqueleto inicial: carga el ROM, descomprime datos de nivel (LZ2),
lee las tablas de punteros y muestra la cabecera primaria de un
nivel. **Todavía no hay renderizado gráfico ni edición** — es el
cimiento sobre el que se construye eso.

## Cómo compilarlo con GitHub Actions (sin instalar nada localmente)

1. Crea un repositorio nuevo en GitHub.
2. Sube todo este contenido tal cual (mantén la estructura de
   carpetas — `.github/workflows/android-build.yml` debe quedar en
   esa ruta exacta).
3. En GitHub, ve a la pestaña **Actions** del repo. Al hacer push a
   `main`, el workflow "Build APK" se dispara solo.
4. Cuando termine (unos minutos), entra al run correspondiente y
   baja hasta **Artifacts** → descarga `smw-editor-debug-apk`. Ahí
   está tu `app-debug.apk`, listo para instalar en el celular
   (activa "orígenes desconocidos" para instalarlo).

También puedes dispararlo manualmente desde Actions → "Build APK" →
"Run workflow", sin necesidad de hacer push.

## Estructura

```
app/src/main/java/com/example/smweditor/
├── MainActivity.kt          # pantalla de prueba: elegir ROM y mostrar cabecera del nivel 0
└── rom/
    ├── RomAddress.kt        # carga de ROM + traducción de direcciones SNES↔archivo
    ├── Lz2.kt                # descompresor LC_LZ2
    └── LevelPointers.kt      # tablas de punteros de nivel + cabecera primaria
```

## Importante

El código Kotlin **no se compiló en este entorno** (no hay
`kotlinc`/Android SDK disponible aquí) — la lógica de descompresión
y direccionamiento la verifiqué aparte en Python contra un stream
sintético, pero la sintaxis Kotlin/Compose en sí la va a validar
GitHub Actions en tu primer push. Si algo falla, el log del Action
te dirá exactamente la línea — pégamelo y lo arreglamos.

## Siguiente paso

Parser de objetos: convertir los bytes crudos que ya podemos leer
(`LevelReader.readObjectBytes()`) en una lista real de "objeto tipo
X en columna/fila Y", que es el mínimo para empezar a dibujar algo
(aunque sea bloques de colores) en pantalla.
