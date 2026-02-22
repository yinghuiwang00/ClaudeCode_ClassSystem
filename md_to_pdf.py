#!/usr/bin/env python3
"""
Convert Markdown file to PDF
"""

from reportlab.lib.pagesizes import letter, A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, HRFlowable
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib import colors
import sys
import re
import os

def escape_xml(text):
    """Escape XML special characters"""
    return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

def process_inline_markdown(text):
    """Process inline markdown (bold, code) - handle code first, then bold"""

    # First, extract code blocks and replace with placeholders
    code_blocks = []
    def save_code(match):
        code_blocks.append(match.group(1))
        return f'__CODE_BLOCK_{len(code_blocks)-1}__'

    # Extract inline code
    text = re.sub(r'`([^`]+)`', save_code, text)

    # Now process bold (outside of code blocks)
    text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)

    # Replace code blocks with formatted version
    for i, code in enumerate(code_blocks):
        text = text.replace(f'__CODE_BLOCK_{i}__', f'<font face="Courier">{escape_xml(code)}</font>')

    return text

def parse_markdown_to_elements(md_text):
    """Parse markdown text and convert to ReportLab flowables"""
    styles = getSampleStyleSheet()
    elements = []

    # Custom styles
    heading1_style = ParagraphStyle(
        'CustomH1',
        parent=styles['Heading1'],
        fontSize=16,
        spaceAfter=12,
        spaceBefore=12,
        textColor=colors.HexColor('#1a1a1a')
    )

    heading2_style = ParagraphStyle(
        'CustomH2',
        parent=styles['Heading2'],
        fontSize=14,
        spaceAfter=10,
        spaceBefore=10,
        textColor=colors.HexColor('#333333')
    )

    heading3_style = ParagraphStyle(
        'CustomH3',
        parent=styles['Heading3'],
        fontSize=12,
        spaceAfter=8,
        spaceBefore=8,
        textColor=colors.HexColor('#555555')
    )

    code_style = ParagraphStyle(
        'Code',
        parent=styles['Code'],
        fontSize=9,
        leftIndent=20,
        backColor=colors.HexColor('#f5f5f5'),
        borderPadding=5,
        leading=12
    )

    normal_style = ParagraphStyle(
        'NormalCustom',
        parent=styles['Normal'],
        leading=14
    )

    # Split into lines
    lines = md_text.split('\n')
    current_block = []
    in_code_block = False
    code_block_lines = []
    in_list = False

    for line in lines:
        # Code block detection
        if line.strip().startswith('```'):
            if in_code_block:
                # End code block
                code_text = '\n'.join(code_block_lines)
                code_para = Paragraph(escape_xml(code_text), code_style)
                elements.append(code_para)
                elements.append(Spacer(1, 6))
                code_block_lines = []
                in_code_block = False
            else:
                # Start code block
                in_code_block = True
            continue

        if in_code_block:
            code_block_lines.append(line)
            continue

        # Empty line - end current paragraph
        if not line.strip():
            if current_block:
                paragraph = ' '.join(current_block)
                para = Paragraph(process_inline_markdown(paragraph), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            in_list = False
            continue

        # Headings
        if line.startswith('### '):
            if current_block:
                para = Paragraph(process_inline_markdown(' '.join(current_block)), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            heading = line[4:]
            elements.append(Paragraph(process_inline_markdown(heading), heading3_style))
            in_list = False
            continue

        if line.startswith('## '):
            if current_block:
                para = Paragraph(process_inline_markdown(' '.join(current_block)), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            heading = line[3:]
            elements.append(Paragraph(process_inline_markdown(heading), heading2_style))
            in_list = False
            continue

        if line.startswith('# '):
            if current_block:
                para = Paragraph(process_inline_markdown(' '.join(current_block)), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            heading = line[2:]
            elements.append(Paragraph(process_inline_markdown(heading), heading1_style))
            in_list = False
            continue

        # Bullet points
        if line.strip().startswith('- '):
            if current_block:
                para = Paragraph(process_inline_markdown(' '.join(current_block)), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            bullet = line.strip()[2:]
            para = Paragraph(f'• {process_inline_markdown(bullet)}', normal_style)
            elements.append(para)
            in_list = True
            continue

        # Numbered list
        if re.match(r'^\d+\.\s', line.strip()):
            if current_block:
                para = Paragraph(process_inline_markdown(' '.join(current_block)), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            item = line.strip()
            para = Paragraph(process_inline_markdown(item), normal_style)
            elements.append(para)
            in_list = True
            continue

        # Horizontal rule
        if line.strip() == '---':
            if current_block:
                para = Paragraph(process_inline_markdown(' '.join(current_block)), normal_style)
                elements.append(para)
                elements.append(Spacer(1, 6))
                current_block = []
            elements.append(HRFlowable(width="100%", thickness=1, lineCap='round', color=colors.grey))
            elements.append(Spacer(1, 12))
            in_list = False
            continue

        # Regular text - accumulate into current paragraph
        current_block.append(line)

    # Handle remaining paragraph
    if current_block:
        paragraph = ' '.join(current_block)
        para = Paragraph(process_inline_markdown(paragraph), normal_style)
        elements.append(para)

    return elements

def markdown_to_pdf(input_file, output_file):
    """Convert markdown file to PDF"""

    # Read markdown file
    with open(input_file, 'r', encoding='utf-8') as f:
        md_text = f.read()

    # Create PDF document
    doc = SimpleDocTemplate(
        output_file,
        pagesize=A4,
        rightMargin=72,
        leftMargin=72,
        topMargin=72,
        bottomMargin=18
    )

    # Parse markdown and create flowables
    elements = parse_markdown_to_elements(md_text)

    # Build PDF
    doc.build(elements)

    print(f"✓ Successfully converted {input_file} to {output_file}")

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python md_to_pdf.py <input.md> [output.pdf]")
        sys.exit(1)

    input_file = sys.argv[1]

    if not os.path.exists(input_file):
        print(f"Error: File '{input_file}' not found")
        sys.exit(1)

    if len(sys.argv) >= 3:
        output_file = sys.argv[2]
    else:
        output_file = os.path.splitext(input_file)[0] + '.pdf'

    markdown_to_pdf(input_file, output_file)
