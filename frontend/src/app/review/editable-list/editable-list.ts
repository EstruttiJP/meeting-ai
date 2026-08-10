import { Component, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Chip } from 'primeng/chip';

@Component({
  selector: 'app-editable-list',
  imports: [FormsModule, Chip],
  templateUrl: './editable-list.html',
  styleUrl: './editable-list.css',
})
export class EditableList {
  readonly label = input.required<string>();
  readonly placeholder = input('Adicionar item e pressionar Enter');
  readonly items = model.required<string[]>();

  protected readonly inputId = `editable-list-${crypto.randomUUID()}`;
  protected draftItem = '';

  protected addItem() {
    const value = this.draftItem.trim();
    if (!value) {
      return;
    }
    this.items.set([...this.items(), value]);
    this.draftItem = '';
  }

  protected removeItem(index: number) {
    this.items.set(this.items().filter((_, i) => i !== index));
  }
}
