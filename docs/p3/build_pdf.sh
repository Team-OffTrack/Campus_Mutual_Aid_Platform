#!/bin/bash
# Convert P3 markdown to PDF with Mermaid diagram rendering
# Requires: @mermaid-js/mermaid-cli (npm), pandoc, xelatex (texlive)
# Install: npm install -g @mermaid-js/mermaid-cli
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
TMPDIR=$(mktemp -d /tmp/p3pdf.XXXXXX)
trap "rm -rf $TMPDIR" EXIT

echo "==> Building PDFs in $DIR"
echo "==> Temp: $TMPDIR"

# Check mmdc
if ! command -v mmdc &>/dev/null; then
    echo "ERROR: mmdc not found. Install with: npm install -g @mermaid-js/mermaid-cli"
    exit 1
fi

# Check pandoc
if ! command -v pandoc &>/dev/null; then
    echo "ERROR: pandoc not found. Install with: sudo pacman -S pandoc-cli"
    exit 1
fi

# Pick pdf engine
if command -v xelatex &>/dev/null; then
    PDF_ENGINE=xelatex
    echo "==> Using $PDF_ENGINE (CJK capable)"
else
    PDF_ENGINE=pdflatex
    echo "==> Using $PDF_ENGINE (CJK may have issues)"
fi

BUILD_COUNT=0
FAIL_COUNT=0

for MD_FILE in "$DIR"/*.md; do
    BASENAME=$(basename "$MD_FILE" .md)
    PDF_FILE="$DIR/$BASENAME.pdf"
    TMP_MD="$TMPDIR/$BASENAME.md"

    echo "--- $BASENAME.md -> $BASENAME.pdf"

    # Pre-process: render mermaid blocks to SVG, replace with image links
    IN_MERMAID=0
    MERMAID_CONTENT=""
    > "$TMP_MD"

    while IFS= read -r line || [ -n "$line" ]; do
        if [ "$IN_MERMAID" -eq 0 ] && [[ "$line" =~ ^'```mermaid' ]]; then
            IN_MERMAID=1
            MERMAID_CONTENT=""
            continue
        fi

        if [ "$IN_MERMAID" -eq 1 ]; then
            if [[ "$line" =~ ^'```' ]]; then
                IN_MERMAID=0
                SVG_FILE="$TMPDIR/${BASENAME}_$(date +%s%N).svg"

                if echo "$MERMAID_CONTENT" | mmdc -o "$SVG_FILE" \
                    -c "$DIR/mermaid-config.json" \
                    -b transparent -w 1200 -t default --quiet 2>/dev/null; then
                    :
                else
                    echo "  WARNING: mmdc with default theme failed, trying neutral..."
                    echo "$MERMAID_CONTENT" | mmdc -o "$SVG_FILE" \
                        -c "$DIR/mermaid-config.json" \
                        -b white -w 1200 --quiet 2>/dev/null || {
                        echo "  WARNING: mmdc failed for a diagram in $BASENAME.md"
                        # Put a placeholder so PDF still builds
                        echo "> *[Mermaid diagram — render failed]*" >> "$TMP_MD"
                        continue
                    }
                fi

                echo "![diagram]($SVG_FILE)" >> "$TMP_MD"
                echo "" >> "$TMP_MD"
            else
                MERMAID_CONTENT+="$line"$'\n'
            fi
        else
            echo "$line" >> "$TMP_MD"
        fi
    done < "$MD_FILE"

    # Convert to PDF
    if [ "$PDF_ENGINE" = "xelatex" ]; then
        pandoc "$TMP_MD" -o "$PDF_FILE" \
            --pdf-engine=xelatex \
            -V mainfont="Noto Serif" \
            -V sansfont="Noto Sans" \
            -V monofont="Noto Sans Mono" \
            -V CJKmainfont="Noto Serif CJK SC" \
            -V CJKsansfont="Noto Sans CJK SC" \
            -V CJKmonofont="Noto Sans Mono CJK SC" \
            -V geometry:margin=2.5cm \
            -V colorlinks=true \
            -V linkcolor=blue \
            --metadata linkcolor=blue \
            -f markdown -t pdf 2>&1 || {
            echo "  ERROR: pandoc failed for $BASENAME.md"
            FAIL_COUNT=$((FAIL_COUNT + 1))
            continue
        }
    else
        pandoc "$TMP_MD" -o "$PDF_FILE" \
            --pdf-engine=pdflatex \
            -V geometry:margin=2.5cm \
            -f markdown -t pdf 2>&1 || {
            echo "  ERROR: pandoc failed for $BASENAME.md"
            FAIL_COUNT=$((FAIL_COUNT + 1))
            continue
        }
    fi

    echo "  -> $PDF_FILE ($(du -h "$PDF_FILE" | cut -f1))"
    BUILD_COUNT=$((BUILD_COUNT + 1))
done

echo "==> Done. Built $BUILD_COUNT PDF(s), $FAIL_COUNT failure(s)."
