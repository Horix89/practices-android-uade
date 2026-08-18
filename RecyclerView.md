# RecyclerView — Explicación paso a paso

---

## El problema que resuelve

Imaginá que tenés una lista de 1000 pokemones. Si crearas un `TextView` por cada uno, tendrías 1000 vistas en memoria al mismo tiempo. Eso mata la performance.

`RecyclerView` resuelve esto con una idea simple: **solo crea las vistas que entran en pantalla** (digamos 8), y cuando una fila desaparece al hacer scroll, la **reutiliza** (la "recicla") para la siguiente.

```
┌─────────────────┐
│   BULBASAUR     │  ← fila visible
│   IVYSAUR       │  ← fila visible
│   VENUSAUR      │  ← fila visible
│   CHARMANDER    │  ← fila visible
│   CHARMELEON    │  ← fila visible
└─────────────────┘
  [scroll hacia abajo]
     ↑ BULBASAUR sale de pantalla → su View se recicla
     ↓ CHARIZARD entra → usa la misma View, solo cambia el texto
```

Por eso se llama **Recycler**View.

---

## Las 4 piezas que necesitás

```
┌──────────────────────────────────────────┐
│             RecyclerView                 │  ← el contenedor (en el XML del Fragment)
│                                          │
│  necesita saber:                         │
│  1. ¿Cómo acomodar las filas?  → LayoutManager
│  2. ¿Qué mostrar en cada fila? → Adapter
└──────────────────────────────────────────┘
```

| Pieza | Qué hace |
|---|---|
| **RecyclerView** | El componente de UI. Es el "contenedor" de la lista |
| **LayoutManager** | Define si las filas van en vertical, horizontal o en grilla |
| **Adapter** | Sabe cuántos ítems hay y cómo dibujar cada uno |
| **ViewHolder** | Guarda las referencias a las vistas de una fila para no buscarlas cada vez |

---

## Paso 1 — El RecyclerView en el layout del Fragment

En `fragment_pokemon_list.xml` declarás el RecyclerView como cualquier otra vista. Solo define el espacio, no sabe qué va a mostrar:

```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rvPokemon"
    android:layout_width="0dp"
    android:layout_height="0dp" />
```

---

## Paso 2 — El layout de cada fila (`item_pokemon.xml`)

Este XML define **cómo se ve UNA sola fila**. En nuestro caso, solo un `TextView`:

```xml
<TextView
    android:id="@+id/tvPokemonName"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="16dp"
    android:textSize="16sp" />
```

No tiene datos. Es solo la "plantilla" vacía de una fila.

---

## Paso 3 — El ViewHolder

Es una clase que **guarda las referencias** a las vistas de una fila. Su único propósito es evitar llamar a `findViewById` cada vez que el RecyclerView recicla una fila (porque `findViewById` es lento).

```java
static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvName;

    ViewHolder(View itemView) {
        super(itemView);
        // Se ejecuta UNA SOLA VEZ cuando se crea la fila
        tvName = itemView.findViewById(R.id.tvPokemonName);
    }
}
```

`itemView` es la fila completa (el `item_pokemon.xml` ya inflado). El ViewHolder busca el `TextView` adentro y lo guarda.

---

## Paso 4 — El Adapter

Es el puente entre **los datos** y **las vistas**. Tiene 3 métodos obligatorios:

```java
public class PokemonAdapter extends RecyclerView.Adapter<PokemonAdapter.ViewHolder> {

    private final List<PokemonResult> items; // los datos

    // ① ¿Cuántas filas hay?
    @Override
    public int getItemCount() {
        return items.size(); // ej: 20
    }

    // ② Crear una fila nueva (solo cuando no hay una para reciclar)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pokemon, parent, false);
        //       ↑ convierte el XML en un objeto View
        return new ViewHolder(view);
        //     ↑ lo envuelve en el ViewHolder
    }

    // ③ Ponerle datos a una fila (se llama cada vez que una fila aparece en pantalla)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.tvName.setText(items.get(position).getName());
        //    ↑ el TextView ya está guardado en el ViewHolder, solo le cambia el texto
    }
}
```

---

## Paso 5 — Conectar todo en el Fragment

```java
RecyclerView rvPokemon = view.findViewById(R.id.rvPokemon);

// 1. Decirle cómo acomodar las filas (vertical, de arriba a abajo)
rvPokemon.setLayoutManager(new LinearLayoutManager(requireContext()));

// 2. Darle el Adapter con los datos
PokemonAdapter adapter = new PokemonAdapter(listaDePokemones);
rvPokemon.setAdapter(adapter);
```

---

## El ciclo completo

```
Fragment
   │
   │ setAdapter(adapter)
   ▼
RecyclerView pregunta: "¿cuántos ítems?" → adapter.getItemCount() → 20
   │
   │ "necesito mostrar fila 0, creá una vista"
   ▼
adapter.onCreateViewHolder()
   │  infla item_pokemon.xml → View
   │  crea ViewHolder → guarda referencia al TextView
   ▼
adapter.onBindViewHolder(holder, position=0)
   │  holder.tvName.setText("bulbasaur")
   ▼
RecyclerView muestra "bulbasaur" en pantalla

   [usuario hace scroll, la fila de "bulbasaur" sale de pantalla]
   │
   │ "tengo esta View libre, reutilizala para la fila 8"
   ▼
adapter.onBindViewHolder(holder, position=8)  ← onCreateViewHolder NO se llama de nuevo
   │  holder.tvName.setText("blastoise")      ← solo cambia el texto
   ▼
RecyclerView muestra "blastoise" en la misma View que era "bulbasaur"
```

---

## En una línea

> **`item_pokemon.xml`** es la plantilla vacía de una fila. El **`ViewHolder`** la guarda lista para usar. El **`Adapter`** le pone los datos. El **`RecyclerView`** la muestra y la recicla.
